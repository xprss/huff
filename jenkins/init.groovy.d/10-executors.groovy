import jenkins.model.Jenkins

def jenkins = Jenkins.get()
jenkins.setNumExecutors(1)
jenkins.save()
