pipeline {
    agent any

    options {
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '50'))
        disableConcurrentBuilds()
        // Speeds up PR feedback if something fails early:
        skipDefaultCheckout(false)
    }

    tools {
        maven 'Maven 3.8.8' // Ensure this matches your Jenkins Maven installation name
        jdk 'JDK 17'        // Ensure this matches your Jenkins JDK installation name
    }

    environment {
        DOCKER_REGISTRY = 'nexus:5000'
    }


    stages {
        stage('Cleanup workspace and Checkout') {
            steps {
                // Note: BRANCH_NAME, GIT_COMMIT and CHANGE_ID are the built-in environment variables
                sh 'echo "Branch: ${BRANCH_NAME:-unknown}; Commit: ${GIT_COMMIT:-unknown}; ChangeID: ${CHANGE_ID:-unknown}"'
                cleanWs()
                checkout scm
            }
        }

        stage('Build and push Docker Image') {
            steps {
                def pom = readMavenPom file: 'pom.xml'
                env.IMAGE_NAME = pom.artifactId
                env.IMAGE_TAG = "${pom.version}-${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
                withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        docker.withRegistry("https://${DOCKER_REGISTRY}", 'nexus-creds') {
                            docker.build("${DOCKER_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}").push()
                        }
                    }
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

        stage('Static Analysis (PMD/Checkstyle/SpotBugs)') {
            steps {
                sh 'mvn integration-test -DskipTests=true -DskipITs=false'
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