def call(String Project, String ImageTag, String DockerHubUser){
  if (!Project?.matches('^[a-zA-Z0-9][a-zA-Z0-9_.-]*$')) {
     error('Invalid project name. Must contain only alphanumeric characters, dots, dashes, and underscores')
  }
  if (!ImageTag?.matches('^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$')) {
     error('Invalid image tag. Must contain only alphanumeric characters, dots, dashes, and underscores')
  }
  if (!DockerHubUser?.matches('^[a-zA-Z0-9][a-zA-Z0-9_.-]*$')) {
     error('Invalid DockerHub user. Must contain only alphanumeric characters, dots, dashes, and underscores')
  }

  withEnv(["DOCKER_IMAGE_REF=${DockerHubUser}/${Project}:${ImageTag}"]) {
      sh 'docker rmi "$DOCKER_IMAGE_REF"'
  }
}
