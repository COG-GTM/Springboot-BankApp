def call() {
    try {
        // Validate environment
        sh 'docker compose config -q'
        
        // Graceful shutdown
        sh 'docker compose down --timeout 30'
        
        // Verify cleanup
        sh '''
            if docker compose ps -q | grep -q .; then
                echo "Failed to stop all containers"
                exit 1
            fi
        '''
        
        sh 'docker compose up -d mysql'
        
        sh 'docker compose up -d springboot-app'
        
        try {
            sh 'docker compose up -d micronaut-app'
            echo "Both Spring Boot and Micronaut applications deployed successfully"
        } catch (Exception micronautError) {
            echo "Defaulting to Spring boot due to micronaut failure: ${micronautError.message}"
        }
        
        sh '''
            max_attempts=30
            attempt=1
            while [ $attempt -le $max_attempts ]; do
                springboot_healthy=$(docker compose ps springboot-app --format '{{.Status}}' | grep -c "(healthy)")
                if [ $springboot_healthy -eq 1 ]; then
                    echo "Spring Boot application is healthy"
                    break
                fi
                echo "Waiting for Spring Boot to be healthy (attempt $attempt/$max_attempts)..."
                sleep 10
                attempt=$((attempt + 1))
            done
            
            if [ $attempt -gt $max_attempts ]; then
                echo "Spring Boot health check timeout after $max_attempts attempts"
                docker compose ps
                exit 1
            fi
        '''
        
        sh '''
            micronaut_running=$(docker compose ps micronaut-app --format '{{.Status}}' | grep -c "Up")
            if [ $micronaut_running -eq 1 ]; then
                echo "Micronaut application is also running"
                max_attempts=15
                attempt=1
                while [ $attempt -le $max_attempts ]; do
                    micronaut_healthy=$(docker compose ps micronaut-app --format '{{.Status}}' | grep -c "(healthy)")
                    if [ $micronaut_healthy -eq 1 ]; then
                        echo "Micronaut application is healthy"
                        break
                    fi
                    echo "Waiting for Micronaut to be healthy (attempt $attempt/$max_attempts)..."
                    sleep 10
                    attempt=$((attempt + 1))
                done
            else
                echo "Micronaut application is not running - continuing with Spring Boot only"
            fi
        '''
        
        echo "Deployment completed successfully"
        
    } catch (Exception e) {
        echo "Deployment failed: ${e.message}"
        throw e
    }
}
