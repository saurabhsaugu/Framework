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
        sh 'mvn -B verify -DskipTests'
        sh 'mvn -B exec:java -Dexec.mainClass=com.company.analytics.FlakyAnalytics'
        archiveArtifacts artifacts: 'target/flaky-summary.json', allowEmptyArchive: true
      }
    }
  }
  post {
    always {
      echo 'Pipeline finished'
    }
  }
}

