package test.artifacts;

import javax.batch.api.Batchlet;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a>
 */
public class WeldArtifactFactoryBatchlet implements Batchlet {

    public static boolean produced = false;

    @Produces
    @Named("weldArtifactFactoryBatchlet")
    public WeldArtifactFactoryBatchlet produce(final @New WeldArtifactFactoryBatchlet that) {
        produced = true;
        return that;
    }

	@Override
	public String process() throws Exception {
        if (!produced) {
            throw new RuntimeException("Not loaded from CDI");
        }
        return null;
	}
	
	@Override
	public void stop() {
        // no op
	}
}