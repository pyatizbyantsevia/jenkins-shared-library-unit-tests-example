package mainpackage

import com.cloudbees.groovy.cps.NonCPS

/**
 * Singleton that provides access to the pipeline steps.
 * <br>
 * Call {@link PipelineContext#init(Script)} is necessary before using any other classes of this library.
 */
class PipelineContext {
    private static final PipelineContext INSTANCE = new PipelineContext()
    private Script pipelineSteps

    private PipelineContext() { }

    @NonCPS
    static PipelineContext getInstance() {
        return INSTANCE
    }

    @NonCPS
    static void init(Script steps) {
        INSTANCE.pipelineSteps = steps
    }

    @NonCPS
    Script getSteps() {
        if (!pipelineSteps) {
            throw new RuntimeException("PipelineContext not initialized. Call PipelineContext#init(Script)")
        }
        return pipelineSteps
    }
}
