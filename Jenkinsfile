@Library('Shared') _
pipeline {
    agent any
    
    environment{
        SONAR_HOME = tool "Sonar"
        // Repository under audit. The pipeline must build this repository and nothing else (ITGC-CM-08).
        APP_REPO_URL = "https://github.com/COG-GTM/Springboot-BankApp.git"
        APP_REPO_BRANCH = "DevOps"
        // Vulnerability gate: any finding at or above this severity fails the build (ITGC-SDLC-09).
        TRIVY_SEVERITY = "HIGH,CRITICAL"
        TRIVY_MISCONFIG_SEVERITY = "CRITICAL"
        OWASP_FAIL_ON_CVSS = "7"
    }
    
    parameters {
        string(name: 'DOCKER_TAG', defaultValue: '', description: 'Setting docker image for latest push')
    }
    
    stages {
        
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
                    code_checkout("${env.APP_REPO_URL}","${env.APP_REPO_BRANCH}")
                }
            }
        }

        stage("Gitleaks: Secret scan"){
            steps{
                script{
                    // Fails the build when credentials are committed to the repository (ITGC-SEC-06).
                    sh '''
                        docker run --rm -v "$PWD:/repo" -w /repo zricethezav/gitleaks:v8.18.4 \
                            detect --source=/repo --no-git --config=/repo/.gitleaks.toml \
                                   --redact --no-banner --exit-code 1 \
                                   --report-format sarif --report-path gitleaks-report.sarif
                    '''
                }
            }
        }

        stage("Maven: Build and unit tests"){
            steps{
                script{
                    // Test gate: the build fails on any failing test (ITGC-SDLC-09).
                    sh './mvnw -B clean verify -DskipITs'
                }
            }
            post{
                always{
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage("Trivy: Filesystem scan"){
            steps{
                script{
                    // Vulnerabilities and secrets are blocking at HIGH,CRITICAL; infrastructure
                    // misconfiguration is blocking at CRITICAL and reported below HIGH.
                    sh """
                        trivy fs . --scanners vuln,secret \
                            --severity ${env.TRIVY_SEVERITY} --ignore-unfixed \
                            --exit-code 1 --no-progress \
                            --format template --template '@/usr/local/share/trivy/templates/junit.tpl' \
                            --output trivy-fs-report.xml
                        trivy fs . --scanners misconfig \
                            --severity ${env.TRIVY_MISCONFIG_SEVERITY} \
                            --exit-code 1 --no-progress \
                            --format table --output trivy-misconfig-report.txt
                    """
                }
            }
        }

        stage("OWASP: Dependency check"){
            steps{
                script{
                    dependencyCheck additionalArguments: "--scan ./ --failOnCVSS ${env.OWASP_FAIL_ON_CVSS} --format XML",
                                    odcInstallation: 'OWASP'
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml',
                                             failedTotalCritical: 0, failedTotalHigh: 0,
                                             unstableTotalMedium: 0
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
                    // Quality gate is blocking: a failed gate stops the pipeline (ITGC-SDLC-09).
                    timeout(time: 5, unit: "MINUTES"){
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        stage("Docker: Build Images"){
            steps{
                script{
                    docker_build("bankapp","${params.DOCKER_TAG}","madhupdevops")
                }
            }
        }
        
        stage("Docker: Push to DockerHub"){
            steps{
                script{
                    docker_push("bankapp","${params.DOCKER_TAG}","madhupdevops")
                }
            }
        }
    }
    post{
        always{
            archiveArtifacts artifacts: 'gitleaks-report.sarif, trivy-fs-report.xml, **/dependency-check-report.xml',
                             allowEmptyArchive: true, followSymlinks: false
        }
        success{
            archiveArtifacts artifacts: '*.xml', followSymlinks: false
            build job: "BankApp-CD", parameters: [
                string(name: 'DOCKER_TAG', value: "${params.DOCKER_TAG}")
            ]
        }
    }
}
