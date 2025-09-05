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

    stages {
        stage('Cleanup workspace and Checkout') {
            steps {
                // Note: BRANCH_NAME, GIT_COMMIT and CHANGE_ID are the built-in environment variables
                sh 'echo "Branch: ${BRANCH_NAME:-unknown}; Commit: ${GIT_COMMIT:-unknown}; ChangeID: ${CHANGE_ID:-unknown}"'
                cleanWs()
                checkout scm
            }
        }

        stage('Build & Unit Tests (JDK 17)') {
            steps {
                sh 'mvn -B -U -DskipITs=true clean verify jacoco:report'
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

        stage('Publish JAR (Maven)') {
            when { branch 'main' }  // for main branch only
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-maven-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                script {
                    // mvn deploy will publish main artifact (JAR) and ATTACHED artifacts (tar.gz) to appropriate repo
                    // snapshots vs releases are chosen by whether version ends with -SNAPSHOT
                    sh '''
                    MVN_ARGS="-DskipTests -DaltDeploymentRepository="
                    VERSION=$(mvn -q -Dexec.cleanupDaemonThreads=false -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:3.1.0:exec)
                    if echo "$VERSION" | grep -q SNAPSHOT; then
                        REPO="internal-snapshots::default::${MVN_SNAPSHOTS_URL}"
                    else
                        REPO="internal-releases::default::${MVN_RELEASES_URL}"
                    fi
                    mvn -B deploy -DskipTests -DaltDeploymentRepository="$REPO" -Dnexus.username="$NEXUS_USER" -Dnexus.password="$NEXUS_PASS"
                    '''
                }
                }
            }
        }
        stage('Publish JAR to Nexus (Maven)') {
            when { branch 'main' }  // publish for PRs (optional), guaranteed for main
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: 'http://localhost:8081',
                        groupId: pom.groupId,
                        version: pom.version,
                        repository: 'maven-releases',
                        credentialsId: 'nexus-creds',
                        artifacts: [[
                        artifactId: pom.artifactId,
                        classifier: '',
                        file: "target/${pom.artifactId}-${pom.version}.jar",
                        type: 'jar'
                        ]])}

            }
        }
    }
}