folder( "draft" ){

}

def folder = "draft"
def build_job = folder + "/build-job"


     freeStyleJob(build_job)
	{
	
	configure { project ->
  project / 'properties' / 'hudson.plugins.copyartifact.CopyArtifactPermissionProperty' / 'projectNameList' {
    'string' "ansible-job"
  }
    }
	triggers{
	 gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(true)
            setBuildDescription(true)
            rebuildOpenMergeRequest('never')
            
       } }
		
	steps {
	
	 scm {
		
        git{
			
		remote{
				url('http://13.56.34.60/gitlab/Surveycorps/TitanClinic-Codes.git')
				credentials('nexus')
			}
			branch('*/master')
			}
    }
	
	
	maven {
		mavenInstallation('ADOP Maven')
		goals('package')
	}
	wrappers {
			preBuildCleanup {
				includePattern('**/target/**')
				deleteDirectories()
				cleanupParameter('CLEANUP')
			}
	publishers {
        downstreamParameterized {
            trigger('code-analysis') {
                condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$BUILD_ID')

                }
            }
        }
    }

	} 
}
}

	
	def tomcat_deploy = folder + "/deploy-tomcat"
	freeStyleJob(tomcat_deploy)
	{
		steps{
			parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
				}
                    customWorkspace('$CUSTOM_WORKSPACE')
					
			
			
			publishers {
			
			downstreamParameterized {
            trigger('nexus_deploy') {
                condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$BUILD_ID')

                  }
                }
              }
			}
		  }
	    }
		
		
		def codeanalysis = folder + "/code-analysis"
	freeStyleJob(codeanalysis)
	{
	customWorkspace('$CUSTOM_WORKSPACE')
configure {
    it / 'builders' << 'hudson.plugins.sonar.SonarRunnerBuilder' {
     properties '''sonar.projectKey=AdopTest
     sonar.projectVersion=$CUSTOM_BUILD_ID
     sonar.projectName=adoptest
	 sonar.web.url=http://13.56.34.60/sonar/
     sonar.sources=.'''
     jdk ('(Inherit From Job)')


    }
  }
	steps{
			
			
				parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
				}
                    customWorkspace('$CUSTOM_WORKSPACE')
	
					
	publishers{
	
          
		 downstreamParameterized {
            trigger('deploy-tomcat') {
            condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$BUILD_ID')

                }
            }
			trigger('failure'){
			condition('FAILED')
			}
      }  }
    }
		}
		
		

def failed = folder + "/failure"
	freeStyleJob(failed)	
	{
	publishers{
	extendedEmail {
            recipientList('afurongamielchrysdin@gmail.com')
            defaultSubject('CODE failed on sonarqube test')
            defaultContent('the code submitted failed to pass the sonarqube test and must be repaired')
            contentType('text/html')
            triggers {
                beforeBuild()
                stillUnstable {
                    subject('ERROR ON BUILD')
                    content('the code submitted failed to pass the sonarqube test and must be repaired')
                    sendTo {
                        developers()
                        requester()
                        culprits()
                    }
                }
            }
        }
		}
	}
	
	
	
	def selenium = folder + "/selenium_draft"
	freeStyleJob(selenium)
	{
	   customWorkspace('$CUSTOM_WORKSPACE')
		steps{
			maven{
				mavenInstallation('ADOP Maven')
		        goals('test')
			}
		}
		publishers{
	
		 downstreamParameterized {
            trigger('nexus_deploy') {
            condition('SUCCESS')
                parameters {
                      predefinedProp('CUSTOM_WORKSPACE', '$WORKSPACE')
					  predefinedProp('CUSTOM_BUILD_ID', '$BUILD_ID')

                }
            }
      }  }
    }
		
	
	
def nexus = folder + "/nexus_deploy"
	freeStyleJob(nexus)
	{
	steps{
	parameters {
				stringParam('CUSTOM_BUILD_ID')
				stringParam('CUSTOM_WORKSPACE')
			}	}
                 
	 customWorkspace('$CUSTOM_WORKSPACE')
		steps {
        nexusArtifactUploader {
        nexusVersion('nexus2')
        protocol('http')
        nexusUrl('13.56.34.60/nexus/content/repositories/releases/')
        groupId('Surveycorps')
        version('$BUILD_ID')
        repository('releases')
        credentialsId('nexus')
		
		artifact {
            artifactId('TitanClinic')
            type('war')
            classifier('')
            file('titanclinic.war')
        }
        }
	     
	}
	}
	
	
	
	def ansible = folder + "/ansible-job"


     freeStyleJob(ansible)
	 {
	 
	 label('ansible')
	  wrappers {
        sshAgent('adop-jenkins-master')
    }
	
	steps {
	scm {
		
        git{
			
		remote{
				url('http://13.56.34.60/gitlab/Surveycorps/TitanClinic-Ansible.git')
				credentials('nexus')		
			}
			branch('*/master')
			}
        copyArtifacts('build-job') {
            includePatterns('titanclinic.war')

            targetDirectory('')
            flatten()
            optional()
            buildSelector {
                workspace()
            }
        }
    }
	shell('ansible-playbook -i hosts playbook.yml -u ec2-user')
	
}	}
	 
	 
	
	def generatepipeview = folder + "/simulationPIPE"
buildPipelineView(generatepipeview) {
    title('simulationPIPE')
    selectedJob(build_job)
    alwaysAllowManualTrigger()
    displayedBuilds(1)
}