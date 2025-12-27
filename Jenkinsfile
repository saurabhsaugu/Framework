pipeline {
  agent any
  options {
    timestamps()
    ansiColor('xterm')
    timeout(time: 60, unit: 'MINUTES')
  }
  parameters {
    booleanParam(name: 'RUN_DOCKER_IMAGE', defaultValue: false, description: 'Build Docker image and run tests inside container')
    string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Desired thread count for TestNG parallel runs (if configured in testng.xml)')
    booleanParam(name: 'HEALENIUM_ENABLED', defaultValue: false, description: 'Enable Healenium wrapper during tests')
    booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run browsers in headless mode')
  }
  environment {
    MAVEN_OPTS = '-Xms256m -Xmx1024m'
    DRIVER = credentials('test-driver')
    MVN_CMD = 'mvn -B'
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }
    stage('Prepare') {
      steps {
        script {
          // Pre-warm Maven repository to reduce CI instability
          sh "${env.MVN_CMD} -DskipTests dependency:go-offline"
        }
      }
    }
    stage('Build') {
      steps {
        sh "${env.MVN_CMD} -DskipTests clean package"
      }
    }
    stage('Run Tests') {
      steps {
        script {
          if (params.RUN_DOCKER_IMAGE) {
            // Build docker image and run tests inside container (image's CMD runs mvn test by default)
            sh 'docker build -t qe-framework:latest .'
            sh "docker run --rm --shm-size=1g -e HEADLESS=${params.HEADLESS} -e THREAD_COUNT=${params.THREAD_COUNT} qe-framework:latest"
          } else {
            // Run tests with explicit TestNG suite and headless flag; pass additional props
            sh "${env.MVN_CMD} -Dsurefire.suiteXmlFiles=testng.xml -Dheadless=${params.HEADLESS} -Dhealenium.enabled=${params.HEALENIUM_ENABLED} -DthreadCount=${params.THREAD_COUNT} test"
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'target/surefire-reports/**, target/*.html, target/*.json, target/cucumber.json', allowEmptyArchive: true
          junit 'target/surefire-reports/*.xml'
          publishHTML(
            target: [
              reportDir: 'target',
              reportFiles: 'extent-report.html',
              reportName: 'Extent Report',
              keepAll: true,
              alwaysLinkToLastBuild: true
            ]
          )
        }
      }
    }
    stage('Flaky Analysis') {
      steps {
        sh "${env.MVN_CMD} verify -DskipTests"
      }
      post {
        always {
          archiveArtifacts artifacts: 'target/flaky/**', allowEmptyArchive: true
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
    stage('Optional: Docker Image Publish') {
      when {
        expression { params.RUN_DOCKER_IMAGE }
      }
      steps {
        echo 'Docker image built and used in Run Tests stage; add registry push steps here if needed.'
      }
    }
  }
  post {
    always {
      echo 'Pipeline finished'
      // Collect diagnostic logs if present
      archiveArtifacts artifacts: 'target/surefire-reports/*.dumpstream', allowEmptyArchive: true
    }
  }
}
