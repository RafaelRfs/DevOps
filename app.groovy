def gitUrl = 'https://github.com/RafaelRfs/apiJavaSpring.git';
def installDir = '/opt/apl/'
def projectName = 'apiJavaSpring';
def fileName = 'api-0.0.1-SNAPSHOT.jar';
def appDir = "${installDir+projectName}/target/${fileName}"
def javaOpts =  '-Xms256M -Xmx1024M'
def description = ' Api Teste CI / CD'
def user='root'
def execStart="java -jar ${installDir+projectName}/target/${fileName} ${javaOpts}"

pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo '[+] Prepare Stage'
                sh "rm -rf ${projectName}"
                sh "rm -rf ${installDir+projectName}"
                sh """
                    export MAVEN_OPTS="-Xms256M -Xmx1024M -XX:MaxPermSize=350m -XX:MaxDirectMemorySize=1024m"
                    if [ ! -d '${installDir}' ]; then
                            mkdir -p ${installDir}
                    fi
                """
            }
        }
        
        stage('Clone') {
            steps {
                echo '[+] Clone Stage '
                sh "git clone ${gitUrl}"
            }
        }

        stage("Restore"){
            steps{
            sh "ls -l ${projectName}";
            echo "Moving to ${installDir}";
            sh "mv ${projectName} ${installDir}"; 
            dir("${installDir+projectName}"){
                sh 'mvn clean'
                }
            }
        }

        stage('Test'){
            steps{
             dir("${installDir+projectName}"){
                    echo ' Testes Unitários Maven ';
                    sh ' mvn test'
                }
            }
        }
           
        stage('Build') {
            steps {
                dir("${installDir+projectName}"){
                    echo 'iniciando o build da aplicação ';
                    sh 'mvn clean package'
                    sh 'mvn clean install'
                }     
            }
        }

        stage('Deploy') {
            steps {
                echo 'Publish '
                sh "ls -l ${installDir+projectName}/target";
                echo "Creating a service instance... "
                dir("${installDir+projectName}"){

                sh "chmod 500 target/${fileName}"

                sh "sudo ln -s ${appDir} /etc/init.d/${projectName}"    

                sh "echo '[Unit]' >> ${projectName}.service"
                sh "echo 'Description=${description}'  >> ${projectName}.service"
                sh "echo '[Service]' >> ${projectName}.service"
                sh "echo 'User=${user}' >> ${projectName}.service"
                sh "echo 'ExecStart=${execStart} SuccessExitStatus=143' >> ${projectName}.service"
                sh "echo '[Install]' >> ${projectName}.service"
                sh "echo 'WantedBy=multi-user.target' >> ${projectName}.service"
                sh "service ${projectName} start"



                }    
            }
        }
     
    }
}
 
