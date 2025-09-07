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
        JENKINS_HOME ='/var/jenkins_home'
    }

    stages {
        stage('Cleanup workspace and Checkout') {
            steps {
                // NOTE: BRANCH_NAME, GIT_COMMIT and CHANGE_ID are the built-in environment variables
                sh 'echo "Branch: ${BRANCH_NAME:-unknown}; Commit: ${GIT_COMMIT:-unknown}; ChangeID: ${CHANGE_ID:-unknown}"'
                cleanWs()
                // NOTE: Shallow clone on job config to speed up checkout
                checkout scm
            }
        }

        stage('Warm Maven cache (once)') {
            agent {
                docker {
                    image "maven:3.9.6-eclipse-temurin-17"
                    args  "-e MAVEN_CONFIG=/mvn/.m2 -e HOME=/mvn -v maven-repo:/mvn/.m2"
                    reuseNode true
                }
            }
            steps {
                // Populate cache without building
                sh 'mvn -B -Dmaven.repo.local=/mvn/.m2/repository dependency:go-offline'
            }
        }

        stage('Static Analysis & Secrets scan in parallel') {
            parallel {
                stage('Static Analysis (Checkstyle/PMD/SpotBugs)') {
                    agent {
                        docker {
                            image "maven:3.9.6-eclipse-temurin-17"
                            args  "-e MAVEN_CONFIG=/mvn/.m2 -e HOME=/mvn -v maven-repo:/mvn/.m2"
                            reuseNode true
                        }
                    }
                    steps {
                        sh "mvn -B -Dmaven.repo.local=/mvn/.m2/repository checkstyle:check pmd:check spotbugs:check"
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
                        sh 'gitleaks detect --source=. --no-banner --redact --exit-code 1'
                    }
                }
            }
        }

        stage('Build, Unit Tests and Coverage') {
            agent {
                docker {
                    image "maven:3.9.6-eclipse-temurin-17"
                    args  "-e MAVEN_CONFIG=/mvn/.m2 -e HOME=/mvn -v maven-repo:/mvn/.m2"
                    reuseNode true
                }
            }
            steps {
                sh 'mvn -B -Dmaven.repo.local=/mvn/.m2/repository clean test jacoco:report package'
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
            agent {
                docker {
                    image "maven:3.9.6-eclipse-temurin-17"
                    args  "-e MAVEN_CONFIG=/mvn/.m2 -e HOME=/mvn -v maven-repo:/mvn/.m2"
                    reuseNode true
                }
            }
            steps {
                sh 'mvn -B -Dmaven.repo.local=/mvn/.m2/repository -DskipTests org.owasp:dependency-check-maven:check -Dformat=HTML'
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
            agent {
                docker {
                    image "maven:3.9.6-eclipse-temurin-17"
                    args  "-e MAVEN_CONFIG=/mvn/.m2 -e HOME=/mvn -v maven-repo:/mvn/.m2"
                    reuseNode true
                }
            }
            // NOTE: Run ITs on PRs and on main branch commits
            when { anyOf { changeRequest(); branch 'main' } }
            steps {
                sh 'mvn -B -Dmaven.repo.local=/mvn/.m2/repository -DskipTests integration-test failsafe:verify'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Image') {
            // NOTE: Build image only on main branch
            when { branch 'main' }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    env.IMAGE_NAME = pom.artifactId
                    env.IMAGE_TAG = "${pom.version}-${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
                    env.FULL_IMAGE = "${DOCKER_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                    env.GIT_SHA = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    env.BUILD_DATE = sh(script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"', returnStdout: true).trim()
                    sh '''
                        docker build \
                            --label org.opencontainers.image.title=${IMAGE_NAME} \
                            --label org.opencontainers.image.source=${GIT_URL} \
                            --label org.opencontainers.image.revision=${GIT_SHA} \
                            --label org.opencontainers.image.created=${BUILD_DATE} \
                            -t ${FULL_IMAGE} .
                    '''
                }
            }
        }

        stage('Trivy Scan and Generate SBOM') {
            // NOTE: Trivay scan image only on main branch
            when { branch 'main' }
            // Run Trivy inside a container; Jenkins auto-mounts $WORKSPACE
            agent {
                docker {
                    image 'aquasec/trivy:0.66.0'
                    // 1) Docker socket so Trivy can see local images built earlier
                    // 2) Named volume for DB cache (faster scans, persists across runs)
                    // 3) (Optional) set HOME to avoid permission oddities on some images
                    args '--entrypoint="" -u 0:0 -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/trivy/.cache -e HOME=/trivy'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    set -e
                    mkdir -p reports

                    # Machine-readable JSON (optional, for Warnings-NG or later processing)
                    trivy image \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --ignore-unfixed \
                        --no-progress \
                        --timeout 10m \
                        --format json \
                        --output reports/trivy.json \
                        --exit-code 1 \
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
                    echo "Vulnerability gate failed. See reports/trivy.json"
                }
            }
        }

        stage('Push Image') {
            // NOTE: Push image only on main branch
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login $DOCKER_REGISTRY --username "$DOCKER_USER" --password-stdin
                        docker push ${FULL_IMAGE}
                    '''
                }
            }
        }
    }
}
