pipeline {
    agent any

    options {
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '50'))
        disableConcurrentBuilds()
        timeout(time: 40, unit: 'MINUTES')
        parallelsAlwaysFailFast()
        // Speeds up PR feedback if something fails early:
        skipDefaultCheckout(false)
    }

    environment {
        DOCKER_REGISTRY = 'localhost:5000'
        USER_HOME = "/home/jenkins"
    }

    stages {
        stage('Cleanup workspace and Checkout') {
            steps {
                // Note: BRANCH_NAME, GIT_COMMIT and CHANGE_ID are the built-in environment variables
                sh 'echo "Branch: ${BRANCH_NAME:-unknown}; Commit: ${GIT_COMMIT:-unknown}; ChangeID: ${CHANGE_ID:-unknown}"'
                cleanWs()
                // Note: Shallow clone to speed up checkout
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: env.BRANCH_NAME]],
                    userRemoteConfigs: [[url: 'https://github.com/daoquocquyen/simple-ci-demo.git']],
                    extensions: [
                        [$class: 'CloneOption', depth: 1, noTags: false, shallow: true]
                    ]
                ])
            }
        }

        stage('Static Analysis (Checkstyle/PMD/SpotBugs)') {
            agent {
                docker {
                    image "maven:3.9.6-eclipse-temurin-17"
                    args  '-v ${HOME}/.m2:${USER_HOME}/.m2:rw'
                    reuseNode true
                }
            }
            steps {
                sh 'ls -la && mvn -B -Duser.home=${USER_HOME} -Dmaven.repo.local=${USER_HOME}/.m2/repository checkstyle:check pmd:check spotbugs:check'
            }
            post {
                always {
                    recordIssues(
                        enabledForFailure: true,
                        tools: [
                            pmdParser(pattern: 'target/pmd.xml'),
                            checkStyle(pattern: 'target/checkstyle-result.xml'),
                            spotBugs(pattern: 'target/spotbugsXml.xml')
                        ]
                    )
                }
            }
        }

        stage('Secrets Scan') {
            agent {
                docker {
                    image 'zricethezav/gitleaks:v8.28.0'
                    args  '--entrypoint=""'
                    reuseNode true
                }
            }
            steps {
                sh 'ls -la && gitleaks detect --source=. --no-banner --redact --exit-code 1'
            }
        }

        stage('Build and push Docker Image') {
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    env.IMAGE_NAME = pom.artifactId
                    env.IMAGE_TAG = "${pom.version}-${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
                    env.FULL_IMAGE = "${DOCKER_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        script {
                            docker.withRegistry("http://${DOCKER_REGISTRY}", 'nexus-creds') {
                                docker.build(env.FULL_IMAGE).push()
                            }
                        }
                    }
                }
            }
        }

        stage('Trivy Scan and Generate SBOM') {
            // Run Trivy inside a container; Jenkins auto-mounts $WORKSPACE
            agent {
                docker {
                image 'aquasec/trivy:0.66.0'
                // 1) Docker socket so Trivy can see local images built earlier
                // 2) Named volume for DB cache (faster scans, persists across runs)
                // 3) (Optional) set HOME to avoid permission oddities on some images
                args '--entrypoint="" -u 0:0 -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache -e HOME=/root'
                }
            }
            environment {
                // Set this earlier after your build; shown here for completeness
                // FULL_IMAGE = "registry.example.com/team/app:${BUILD_NUMBER}"
                TRIVY_SEVERITY = 'HIGH,CRITICAL'
            }
            steps {
                sh '''
                set -e
                mkdir -p reports

                # Machine-readable JSON (optional, for Warnings-NG or later processing)
                trivy image \
                    --scanners vuln \
                    --severity "${TRIVY_SEVERITY}" \
                    --ignore-unfixed \
                    --no-progress \
                    --timeout 10m \
                    --format json \
                    --output reports/trivy.json \
                    "${FULL_IMAGE}"

                # CycloneDX SBOM alongside the scan
                trivy image --format cyclonedx --output reports/sbom-image-cyclonedx.json "${FULL_IMAGE}"
                '''
            }
            post {
                always {
                    archiveArtifacts(artifacts: 'reports/sbom-image-cyclonedx.json', fingerprint: true)
                    recordIssues(tools: [trivy(pattern: 'reports/trivy.json')])
                }
                failure {
                    echo "Vulnerability gate failed. See reports/trivy-table.txt"
                }
            }
        }

        stage('Build & Unit Tests (JDK 17)') {
            steps {
                sh 'mvn -B -U -DskipITs=true clean verify jacoco:report package'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    publishHTML(target: [
                        reportName: 'JaCoCo (Unit)',
                        reportDir : 'target/site/jacoco',
                        reportFiles: 'index.html',
                        keepAll: true, alwaysLinkToLastBuild: true
                    ])
                }
            }
        }



        stage('Dependency Audit (OWASP)') {
            steps {
                sh 'mvn -B -DskipTests org.owasp:dependency-check-maven:check -Dformat=HTML'
            }
            post {
                always {
                publishHTML(target: [
                    reportName: 'OWASP Dependency-Check',
                    reportDir : 'target',
                    reportFiles: 'dependency-check-report.html',
                    keepAll: true, alwaysLinkToLastBuild: true
                ])
                }
            }
        }

        stage('Integration Tests') {
            // Run ITs on PRs and on main branch commits
            when { anyOf { changeRequest(); branch 'main' } }
            steps {
                sh 'mvn -B -DskipITs=false verify'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                }
            }
        }
    }
}