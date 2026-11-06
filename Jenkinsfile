@Library('Shared') _
pipeline {
    agent any
    
    environment{
        SONAR_HOME = tool "Sonar"
    }
    
    parameters {
        string(name: 'DOCKER_TAG', defaultValue: '', description: 'Docker image tag to build and push (letters, digits, ".", "_", "-"; max 128 chars)')
    }
    
    stages {
        
        stage("Validate: Parameters"){
            steps{
                script{
                    if (!(params.DOCKER_TAG ==~ /^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$/)) {
                        error('Invalid DOCKER_TAG: must match ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$')
                    }
                }
            }
        }
        
        stage("Workspace cleanup"){
            steps{
                script{
                    cleanWs()
                }
            }
        }
        
        stage('Git: Code Checkout') {
            steps {
                script{
                    code_checkout("https://github.com/LondheShubham153/Springboot-BankApp.git","DevOps")
                }
            }
        }
        
        stage("Trivy: Filesystem scan"){
            steps{
                script{
                    trivy_scan()
                }
            }
        }

        stage("OWASP: Dependency check"){
            steps{
                script{
                    owasp_dependency()
                }
            }
        }
        
        stage("SonarQube: Code Analysis"){
            steps{
                script{
                    sonarqube_analysis("Sonar","bankapp","bankapp")
                }
            }
        }
        
        stage("SonarQube: Code Quality Gates"){
            steps{
                script{
                    sonarqube_code_quality()
                }
            }
        }

        stage("Docker: Build Images"){
            steps{
                script{
                    docker_build("bankapp", params.DOCKER_TAG, "madhupdevops")
                }
            }
        }
        
        stage("E2E: Cypress smoke suite"){
            steps{
                script{
                    dir('e2e'){
                        sh '''
                            npm ci
                            npm run stack:up
                            npm run stack:seed
                            npm run cy:run
                        '''
                    }
                }
            }
            post{
                failure{
                    sh 'cd e2e && npm run -s stack:logs > stack.log 2>&1 || true'
                    archiveArtifacts artifacts: 'e2e/cypress/screenshots/**/*, e2e/cypress/videos/**/*, e2e/stack.log', allowEmptyArchive: true, followSymlinks: false
                }
                always{
                    sh 'cd e2e && npm run stack:down || true'
                }
            }
        }
        
        stage("Docker: Push to DockerHub"){
            steps{
                script{
                    docker_push("bankapp", params.DOCKER_TAG, "madhupdevops")
                }
            }
        }
    }
    post{
        success{
            archiveArtifacts artifacts: '*.xml', followSymlinks: false
            build job: "BankApp-CD", parameters: [
                string(name: 'DOCKER_TAG', value: params.DOCKER_TAG)
            ]
        }
    }
}
