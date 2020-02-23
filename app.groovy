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
def envOpts = """
                    export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=com.google.guice,javassist"
                    export MAVEN_OPTS="-Xms8G -Xmx8G -XX:MaxPermSize=2048m -XX:MaxDirectMemorySize=2048m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
              """

pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo '[+] Prepare Stage'
                sh "rm -rf ${projectName}"
                sh "rm -rf ${installDir+projectName}"
                sh "rm -rf ${installDir+projectName}@tmp"
                sh "rm -rf /var/lib/jenkins/.m2/repository"
                sh """
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
                sh 'mvn help:evaluate -Dexpression=settings.localRepository'
                sh 'mvn --version'
                sh 'mvn clean'
                }
            }
        }

        stage('Test'){
            steps{
             dir("${installDir+projectName}"){
                    sh "${envOpts}"
                    echo ' Testes Unitários Maven ';
                    sh ' mvn  test'
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
                sh "chmod 500 target/${fileName}"
                sh "rm -rf  ${serviceFile} "
                sh "echo '[Unit]' >> ${serviceFile}"
                sh "echo 'Description=${description}'  >>  ${serviceFile}"
                sh "echo '[Service]' >>  ${serviceFile}"
                sh "echo 'User=${user}' >>  ${serviceFile}"
                sh "echo 'ExecStart=${execStart} SuccessExitStatus=143' >>  ${serviceFile}"
                sh "echo '[Install]' >>  ${serviceFile}"
                sh "echo 'WantedBy=multi-user.target' >>  ${serviceFile}"
                sh "chmod +x +r  ${serviceFile}"
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
 
