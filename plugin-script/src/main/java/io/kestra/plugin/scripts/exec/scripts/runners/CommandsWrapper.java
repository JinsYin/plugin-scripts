package io.kestra.plugin.scripts.exec.scripts.runners;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.tasks.NamespaceFiles;
import io.kestra.core.runners.FilesService;
import io.kestra.core.runners.NamespaceFilesService;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.RunnerType;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.services.ScriptService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kestra.core.utils.Rethrow.throwFunction;

@AllArgsConstructor
@Getter
public class CommandsWrapper {
    private RunContext runContext;

    private Path workingDirectory;

    private Path outputDirectory;

    private Map<String, Object> additionalVars;

    private List<String> commands;

    private Map<String, String> env;

    @With
    private AbstractLogConsumer logConsumer;

    @With
    private RunnerType runnerType;

    @With
    private DockerOptions dockerOptions;

    @With
    private Boolean warningOnStdErr;

    @With
    private NamespaceFiles namespaceFiles;

    @With
    private Object inputFiles;

    @With
    private List<String> outputFiles;

    public CommandsWrapper(RunContext runContext) {
        this.runContext = runContext;

        this.workingDirectory = runContext.tempDir();
        this.outputDirectory = this.workingDirectory.resolve(IdUtils.create());
        //noinspection ResultOfMethodCallIgnored
        this.outputDirectory.toFile().mkdirs();

        this.additionalVars = new HashMap<>(Map.of(
            "workingDir", workingDirectory.toAbsolutePath().toString(),
            "outputDir", outputDirectory.toString()
        ));

        this.logConsumer = new DefaultLogConsumer(runContext);
    }

    public CommandsWrapper withCommands(List<String> commands) throws IOException, IllegalVariableEvaluationException {
        return new CommandsWrapper(
            runContext,
            workingDirectory,
            outputDirectory,
            additionalVars,
            ScriptService.uploadInputFiles(runContext, runContext.render(commands, this.additionalVars)),
            env,
            logConsumer,
            runnerType,
            dockerOptions,
            warningOnStdErr,
            namespaceFiles,
            inputFiles,
            outputFiles
        );
    }

    public CommandsWrapper withEnv(Map<String, String> envs) throws IllegalVariableEvaluationException {
        return new CommandsWrapper(
            runContext,
            workingDirectory,
            outputDirectory,
            additionalVars,
            commands,
            (envs == null ? Map.<String, String>of() : envs)
                .entrySet()
                .stream()
                .map(throwFunction(r -> new AbstractMap.SimpleEntry<>(
                        runContext.render(r.getKey(), additionalVars),
                        runContext.render(r.getValue(), additionalVars)
                    )
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            logConsumer,
            runnerType,
            dockerOptions,
            warningOnStdErr,
            namespaceFiles,
            inputFiles,
            outputFiles
        );
    }

    public CommandsWrapper addAdditionalVars(Map<String, Object> additionalVars) {
        this.additionalVars.putAll(additionalVars);

        return this;
    }

    public CommandsWrapper addEnv(Map<String, String> envs) {
        this.env.putAll(envs);

        return this;
    }

    @SuppressWarnings("unchecked")
    public ScriptOutput run() throws Exception {
        RunnerResult runnerResult;

        if (this.namespaceFiles != null) {
            String tenantId = ((Map<String, String>) runContext.getVariables().get("flow")).get("tenantId");
            String namespace = ((Map<String, String>) runContext.getVariables().get("flow")).get("namespace");

            NamespaceFilesService namespaceFilesService = runContext.getApplicationContext().getBean(NamespaceFilesService.class);
            namespaceFilesService.inject(
                runContext,
                tenantId,
                namespace,
                this.workingDirectory,
                this.namespaceFiles
            );
        }

        if (this.inputFiles != null) {
            FilesService.inputFiles(runContext, this.inputFiles);
        }

        if (runnerType.equals(RunnerType.DOCKER)) {
            runnerResult = new DockerScriptRunner(runContext.getApplicationContext()).run(this, this.dockerOptions);
        } else {
            runnerResult = new ProcessBuilderScriptRunner().run(this);
        }

        Map<String, URI> outputFiles = ScriptService.uploadOutputFiles(runContext, outputDirectory);

        if (this.outputFiles != null) {
            outputFiles.putAll(FilesService.outputFiles(runContext, this.outputFiles));
        }

        return ScriptOutput.builder()
            .exitCode(runnerResult.getExitCode())
            .stdOutLineCount(runnerResult.getLogConsumer().getStdOutCount())
            .stdErrLineCount(runnerResult.getLogConsumer().getStdErrCount())
            .warningOnStdErr(this.warningOnStdErr)
            .vars(runnerResult.getLogConsumer().getOutputs())
            .outputFiles(outputFiles)
            .build();
    }
}

