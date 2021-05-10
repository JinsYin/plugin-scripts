package io.kestra.plugin.scripts;

import io.kestra.core.runners.RunContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;

import javax.script.*;

public abstract class ScriptEngineService {
    public static CompiledScript scripts(RunContext runContext, String engineName, String script) throws ScriptException {
        Logger logger = runContext.logger();

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName(engineName);

        Bindings bindings = engine.createBindings();

        runContext
            .getVariables()
            .forEach(bindings::put);
        bindings.put("runContext", runContext);
        bindings.put("logger", logger);

        return new CompiledScript(engine, ((Compilable) engine).compile(script), bindings);
    }

    @Getter
    @AllArgsConstructor
    static class CompiledScript {
        private final ScriptEngine engine;
        private final javax.script.CompiledScript script;
        private final Bindings bindings;
    }
}