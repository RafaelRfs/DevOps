def gitUrl = 'https://github.com/RafaelRfs/apiJavaSpring.git';
def installDir = '/opt/apl/'
def projectName = 'apiJavaSpring';
def fileName = 'api-0.0.1-SNAPSHOT.jar';
def appDir = "${installDir+projectName}/target/${fileName}"
def javaOpts =  '-Xms256M -Xmx1024M'
def description = ' Api Teste CI / CD'
def user='root'
def execStart="java -jar ${installDir+projectName}/target/${fileName} ${javaOpts}"
def serviceFile = "/etc/systemd/system/${projectName}.service"
def skipTests =  true
def envOpts = ""
/*
def envOpts = """
                    export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=com.google.guice,javassist"
                    export MAVEN_OPTS="-Xms8G -Xmx8G -XX:MaxPermSize=2048m -XX:MaxDirectMemorySize=2048m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
              """
*/

pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo '[+] Prepare Stage'
                sh """
                rm -rf /var/cache/jenkins/*
                rm -rf ${projectName}
                rm -rf ${installDir+projectName}
                rm -rf ${installDir+projectName}@tmp
                rm -rf /var/lib/jenkins/.m2/repository
                rm -rf /var/lib/home/.m2/repository
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
                    if(skipTests == false) sh ' mvn  test'
                }
            }
        }
           
        stage('Build') {
            steps {
                dir("${installDir+projectName}"){
                    echo 'iniciando o build da aplicação ';
                    sh "${envOpts}"
                    sh 'mvn clean install -Dskiptests=true'
                }     
            }
        }

        stage('Publish') {
            steps {
                sh "ls -l ${installDir+projectName}/target";
                echo "Creating a service instance... "
                dir("${installDir+projectName}"){
                sh """
                chmod 500 target/${fileName}
                rm -rf  ${serviceFile}
                touch  ${serviceFile}
                echo '[Unit]' >> ${serviceFile}
                echo 'Description=${description}'  >>  ${serviceFile}
                echo '[Service]' >>  ${serviceFile}
                echo 'User=${user}' >>  ${serviceFile}
                echo 'ExecStart=${execStart} SuccessExitStatus=143' >>  ${serviceFile}
                echo '[Install]' >>  ${serviceFile}
                echo 'WantedBy=multi-user.target' >>  ${serviceFile}
                chmod +x +r  ${serviceFile}
                """
                }    
            }
        }

        stage('Deploy') {
            steps {
                 echo  "Running the Service Aplication"
                 sh "service ${projectName} start"
            }
        }
     
    }
}
 
