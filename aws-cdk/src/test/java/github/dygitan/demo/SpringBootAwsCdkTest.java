package github.dygitan.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import github.dygitan.demo.aws.cdk.SpringBootAwsCdkStack;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.App;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringBootAwsCdkTest {

    private final static ObjectMapper JSON =
            new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() {
        App app = new App();
        SpringBootAwsCdkStack stack = new SpringBootAwsCdkStack(app, "test");

        // synthesize the stack to a CloudFormation template
        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

        assertThat(actual.get("Resources")).isNotEmpty();
    }
}
