pipeline {
  agent any
  environment {
    MAVEN_OPTS = '-Xms256m -Xmx1024m'
    DRIVER = credentials('test-driver') // optional credential example
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    stage('Build') {
      steps {
        sh 'mvn -B -DskipTests clean package'
      }
    }
    stage('Run Tests') {
      steps {
        // Run tests; override driver via -Ddriver=mobile or web
        sh 'mvn -B test -Ddriver=web'
      }
      post {
        always {
          archiveArtifacts artifacts: 'target/*.xml, target/*.json, target/*.html', allowEmptyArchive: true
          junit 'target/surefire-reports/*.xml'
        }
      }
    }
    stage('Flaky Analysis') {
      steps {
        // run verify which will also execute analytics via exec plugin (if configured)
        sh 'mvn -B verify -DskipTests'
      }
      post {
        always {
          // Archive raw flaky log and generated reports
          archiveArtifacts artifacts: 'target/flaky/**', allowEmptyArchive: true
          // Publish HTML report (requires HTML Publisher plugin installed on Jenkins)
          publishHTML([
            reportDir: 'target/flaky',
            reportFiles: 'flaky-report.html',
            reportName: 'Flaky Tests Report',
            keepAll: true,
            alwaysLinkToLastBuild: true
          ])
        }
      }
    }
  }
  post {
    always {
      echo 'Pipeline finished'
    }
  }
}
