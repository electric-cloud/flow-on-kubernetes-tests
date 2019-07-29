Feature: Check Deployment flow

  Background: Cluster and Helm tiller are ready
    Given Cluster provisioned
    And Helm installed
    And flow-on-kubernetes repo cloned
    And tiller deployed to the cluster
    And I run command kubectl patch deploy --namespace kube-system tiller-deploy -p {"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}  --v=8 in directory flow-on-kubernetes/charts

  Scenario: 01 - User can deploy flow
    # Edit value.yaml
    And I replace host: fbapp.ecsaas.xyz value to host: test.ecsaas.xyz value in flow-on-kubernetes/charts/cloudbees-flow/values.yaml
    And I replace externalEndpoint: "external-db-endpoint" value to externalEndpoint: <rds-endpoint> value in flow-on-kubernetes/charts/cloudbees-flow/values.yaml
    And I replace storageClass: efs-storage value to storageClass: aws-efs-storage value in flow-on-kubernetes/charts/cloudbees-flow/values.yaml
    And I execute command helm lint flow-on-kubernetes/charts/cloudbees-flow
    Then output contains: 1 chart(s) linted, no failures
    # Deploy flow
    When I run command helm install --name cloudbees-flow cloudbees-flow  --namespace test-chart in directory flow-on-kubernetes/charts