package github.dygitan.demo.aws.cdk;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class SpringBootAwsCdkApp {

    public static void main(final String[] args) {
        App app = new App();

        new SpringBootAwsCdkStack(app, "spring-boot-aws-cdk-app",
                StackProps
                        .builder()
                        .env(Environment.builder()
                                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                                        .region(System.getenv("CDK_DEFAULT_REGION"))
                                        .build())
                        .build());

        app.synth();
    }
}
