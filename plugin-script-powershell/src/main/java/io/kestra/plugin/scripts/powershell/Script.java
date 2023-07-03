package io.kestra.plugin.scripts.powershell;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.Commands;
import io.kestra.plugin.scripts.exec.scripts.services.ScriptService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Execute a PowerShell script."
)
public class Script extends AbstractExecScript {
    @Schema(
        title = "The script file to run"
    )
    @PluginProperty(dynamic = true)
    protected String script;

    @Override
    protected DockerOptions defaultDockerOptions() {
        return DockerOptions.builder()
            .image("mcr.microsoft.com/powershell")
            .build();
    }

    @Builder.Default
    @Schema(
        title = "Interpreter to used"
    )
    @PluginProperty
    @NotNull
    @NotEmpty
    protected List<String> interpreter = List.of("pwsh", "-NoProfile", "-NonInteractive", "-Command");

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        Commands commands = this.commands(runContext);

        Path path = runContext.tempFile(
            runContext.render(this.script, commands.getAdditionalVars()).getBytes(StandardCharsets.UTF_8),
            ".ps1"
        );

        List<String> commandsArgs = ScriptService.scriptCommands(
            this.interpreter,
            this.beforeCommands,
            path.toAbsolutePath().toString()
        );

        return commands
            .withCommands(commandsArgs)
            .run();
    }
}