def call(String Project, String ImageTag, String dockerhubuser){
  if (!Project?.matches('^[a-zA-Z0-9][a-zA-Z0-9_.-]*$')) {
     error('Invalid project name. Must contain only alphanumeric characters, dots, dashes, and underscores')
  }
  if (!ImageTag?.matches('^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$')) {
     error('Invalid image tag. Must contain only alphanumeric characters, dots, dashes, and underscores')
  }
  if (!dockerhubuser?.matches('^[a-zA-Z0-9][a-zA-Z0-9_.-]*$')) {
     error('Invalid DockerHub user. Must contain only alphanumeric characters, dots, dashes, and underscores')
  }

  withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'dockerhubpass', usernameVariable: 'dockerhubuser')]) {
      sh 'echo "$dockerhubpass" | docker login -u "$dockerhubuser" --password-stdin'
  }

  withEnv(["DOCKER_IMAGE_REF=${dockerhubuser}/${Project}:${ImageTag}"]) {
      sh 'docker push "$DOCKER_IMAGE_REF"'
  }
}
