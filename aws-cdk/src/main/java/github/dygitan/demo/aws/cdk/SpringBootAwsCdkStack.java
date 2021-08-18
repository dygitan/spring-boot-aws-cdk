package github.dygitan.demo.aws.cdk;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplication;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplicationVersion;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplicationVersion.SourceBundleProperty;
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate;
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty;
import software.amazon.awscdk.services.elasticbeanstalk.CfnEnvironment;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.iam.CfnRole.PolicyProperty;
import software.amazon.awscdk.services.s3.assets.Asset;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class SpringBootAwsCdkStack extends Stack {

    public SpringBootAwsCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SpringBootAwsCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Asset asset = Asset.Builder.create(this, "SpringBootAppArtifact")
                                   .path(new File("../spring-boot/target/spring-boot-0.0.1-SNAPSHOT.jar").toString())
                                   .build();

        CfnApplication cfnApplication = CfnApplication.Builder.create(this, "SpringBootApp")
                                                              .applicationName("spring-boot-app")
                                                              .build();

        CfnApplicationVersion cfnApplicationVersion = CfnApplicationVersion.Builder
                .create(this, "SpringBootAppVersion")
                .applicationName(cfnApplication.getApplicationName())
                .sourceBundle(SourceBundleProperty.builder()
                                                  .s3Bucket(asset.getS3BucketName())
                                                  .s3Key(asset.getS3ObjectKey())
                                                  .build())
                .build();

        CfnRole cfnRole = CfnRole.Builder
                .create(this, "SpringBootAppInstanceRole")
                .assumeRolePolicyDocument(PolicyDocument.Builder
                        .create()
                        .statements(List.of(
                                PolicyStatement.Builder
                                        .create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("sts:AssumeRole"))
                                        .principals(List.of(ServicePrincipal.Builder
                                                .create("ec2.amazonaws.com")
                                                .build()))
                                        .build()
                        ))
                        .build())
                .managedPolicyArns(List.of(
                        "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier"
                ))
                .path("/")
                .policies(List.of(PolicyProperty.builder()
                                                .policyName("S3Access")
                                                .policyDocument(PolicyDocument.Builder
                                                        .create()
                                                        .statements(List.of(
                                                                PolicyStatement.Builder
                                                                        .create()
                                                                        .effect(Effect.ALLOW)
                                                                        .actions(List.of("s3:*"))
                                                                        .resources(List.of(asset.getBucket().getBucketArn()))
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build()))
                .build();

        CfnInstanceProfile cfnInstanceProfile = CfnInstanceProfile.Builder
                .create(this, "SpringBootAppInstanceProfile")
                .instanceProfileName("spring-boot-app-instance-profile")
                .roles(List.of(cfnRole.getRef()))
                .build();

        List<ConfigurationOptionSettingProperty> options = List.of(
                ConfigurationOptionSettingProperty.builder()
                                                  .namespace("aws:autoscaling:asg")
                                                  .optionName("MinSize")
                                                  .value("1")
                                                  .build(),
                ConfigurationOptionSettingProperty.builder()
                                                  .namespace("aws:autoscaling:asg")
                                                  .optionName("MaxSize")
                                                  .value("1")
                                                  .build(),
                ConfigurationOptionSettingProperty.builder()
                                                  .namespace("aws:elasticbeanstalk:environment")
                                                  .optionName("EnvironmentType")
                                                  .value("SingleInstance")
                                                  .build(),
                ConfigurationOptionSettingProperty.builder()
                                                  .namespace("aws:autoscaling:launchconfiguration")
                                                  .optionName("InstanceType")
                                                  .value("t2.micro")
                                                  .build(),
                ConfigurationOptionSettingProperty.builder()
                                                  .namespace("aws:autoscaling:launchconfiguration")
                                                  .optionName("IamInstanceProfile")
                                                  .value(cfnInstanceProfile.getRef())
                                                  .build()
        );

        CfnConfigurationTemplate cfnConfigurationTemplate = CfnConfigurationTemplate.Builder
                .create(this, "SpringBootAppConfigTemplate")
                .applicationName(cfnApplication.getRef())
                .solutionStackName("64bit Amazon Linux 2 v3.2.4 running Corretto 11")
                .optionSettings(options)
                .build();

        CfnEnvironment.Builder.create(this, "SpringBootAppEnv")
                              .applicationName(cfnApplication.getRef())
                              .environmentName("spring-boot-app-env")
                              .templateName(cfnConfigurationTemplate.getRef())
                              .versionLabel(cfnApplicationVersion.getRef())
                              .build();

        cfnApplicationVersion.addDependsOn(cfnApplication);
    }
}
