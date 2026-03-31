// Define function
def call(String project_name, String image_tag, String docker_hub_user){
  sh "docker build -t ${docker_hub_user}/${project_name}:${image_tag} ."
}
