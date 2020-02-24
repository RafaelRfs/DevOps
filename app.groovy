def gitUrl = 'https://github.com/RafaelRfs/apiJavaSpring.git';
def installDir = '/opt/apl/'
def projectName = 'apiJavaSpring';
def fileName = 'api-0.0.1-SNAPSHOT.jar';
def appDir = "${installDir+projectName}/target/${fileName}"
def javaOpts =  '-Xms256M -Xmx1024M'
def description = ' Api Teste CI / CD'
def user='root'
def execStart="/usr/bin/java -jar ${installDir+projectName}/target/${fileName} ${javaOpts}"
def sysDir = "/etc/systemd/system/"
def serviceFile = "${projectName}.service"
def skipTests =  true
def envOpts = """
                    export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=com.google.guice,javassist"
                    export MAVEN_OPTS="-Xmx8G -XX:MaxDirectMemorySize=2048m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
              """

pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo '[+] Prepare Stage'
                sh """
                sudo rm -rf ${projectName}
                sudo rm -rf ${installDir+projectName}
                sudo rm -rf ${installDir+projectName}@tmp
                if service --status-all | grep -Fq '${projectName}'; then    
                    sudo service ${projectName} stop    
                fi
                ${envOpts}
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
                sh 'mvn --version'
                }
            }
        }

        stage('Test'){
            steps{
             dir("${installDir+projectName}"){
                    sh "${envOpts}"
                    echo ' Testes Unitários Maven ';
                    //if(skipTests == false) sh ' mvn  test'
                }
            }
        }
           
        stage('Build') {
            steps {
                dir("${installDir+projectName}"){
                    echo 'iniciando o build da aplicação ';
                    sh "${envOpts}"
                    sh 'sudo mvn clean install -Dskiptests=true'
                }     
            }
        }

        stage('Publish') {
            steps {
                sh "ls -l ${installDir+projectName}/target";
                echo "Creating a service instance... "
                dir("${installDir+projectName}"){
                sh """
                sudo chmod 500 target/${fileName}
                sudo rm -rf  ${serviceFile}
                sudo echo '[Unit]' >> ${serviceFile}
                sudo echo 'Description=${description}'  >>  ${serviceFile}
                sudo echo '[Service]' >>  ${serviceFile}
                sudo echo 'User=${user}' >>  ${serviceFile}
                sudo echo 'ExecStart=${execStart} SuccessExitStatus=143' >>  ${serviceFile}
                sudo echo '[Install]' >>  ${serviceFile}
                sudo echo 'WantedBy=multi-user.target' >>  ${serviceFile}
                sudo chmod +x  ${serviceFile}
                sudo mv ${serviceFile}  ${sysDir}
                ls -l ${sysDir}
                """
                }    
            }
        }

        stage('Deploy') {
            steps {
                 echo  "Running the Service Aplication"
                 sh "sudo service ${projectName} start"
                 sh "sudo service status ${projectName}"
                 echo "Deploy Sucess"
            }
        }
     
    }
}
 
