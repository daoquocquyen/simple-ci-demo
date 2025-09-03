pipeline {
    agent any

    tools {
        maven 'Maven 3.8.8' // Ensure this matches your Jenkins Maven installation name
        jdk 'JDK 17'        // Ensure this matches your Jenkins JDK installation name
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Code Quality') {
            steps {
                sh 'mvn spotbugs:check checkstyle:check'
            }
        }
        stage('UT/IT Test & Code Coverage') {
            steps {
                sh 'mvn verify'
            }
        }
        stage('Deploy') {
            when {
                branch 'main'
                not { changeRequest() }
            }
            steps {
                echo 'Deploying Spring Boot app...'
                // Example: sh 'scp target/*.jar user@server:/deploy/path'
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true

            // Publish SpotBugs and Checkstyle reports to Jenkins
            recordIssues tools: [spotBugs(pattern: '**/target/spotbugsXml.xml'), checkStyle(pattern: '**/target/checkstyle-result.xml')]
        }
        failure {
            mail to: 'dev-team@example.com',
                 subject: "Build failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Check Jenkins for details."
        }
    }
}