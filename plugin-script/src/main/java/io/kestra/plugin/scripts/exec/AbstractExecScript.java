package io.kestra.plugin.scripts.exec;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.RunnerType;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.Commands;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractExecScript extends Task implements RunnableTask<ScriptOutput> {
    @Builder.Default
    @Schema(
        title = "Runner to use"
    )
    @PluginProperty
    @NotNull
    @NotEmpty
    protected RunnerType runner = RunnerType.DOCKER;

    @Schema(
        title = "Docker options when using runner `DOCKER`"
    )
    @PluginProperty
    protected DockerOptions docker;

    @Schema(
        title = "The commands to run before"
    )
    @PluginProperty(dynamic = true)
    protected List<String> beforeCommands;

    @Schema(
        title = "Additional environments variable to add for current process."
    )
    @PluginProperty(
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Builder.Default
    @Schema(
        title = "Use `WARNING` state if any stdErr is sent"
    )
    @PluginProperty
    @NotNull
    protected Boolean warningOnStdErr = true;

    @Builder.Default
    @Schema(
        title = "Interpreter to used"
    )
    @PluginProperty
    @NotNull
    @NotEmpty
    protected List<String> interpreter = List.of("/bin/sh", "-c");

    protected abstract DockerOptions defaultDockerOptions();

    protected Commands commands(RunContext runContext) throws IllegalVariableEvaluationException {
        Commands commands = new Commands(runContext, this.defaultDockerOptions())
            .withEnv(this.env)
            .withWarningOnStdErr(this.warningOnStdErr)
            .withRunnerType(this.runner);

        if (this.docker != null) {
            commands = commands.withDockerOptions(this.docker);
        }

        return commands;
    }
}