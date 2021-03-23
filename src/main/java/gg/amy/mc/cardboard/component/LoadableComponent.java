package gg.amy.mc.cardboard.component;

/**
 * @author amy
 * @since 1/9/20.
 */
public abstract class LoadableComponent {
    public final boolean doInit() {
        loadConfig();
        return init();
    }
    
    /**
     * This method is a no-op by default; most configuration should be
     * trivially autoconfigured via {@link gg.amy.mc.cardboard.config.Config}.
     * If there's something complicated that can't be autoinjected, then this
     * method is used for loading such data.
     * <p/>
     * This method is always called before {@link #init()}.
     */
    public void loadConfig() {
    }
    
    /**
     * Inits this component very early in the init process. This only applies
     * to {@link Single} components. You might use this if you're messing with
     * the internals of the server.
     * <p/>
     * You probably don't want to use this.
     */
    public final void earlyInit() {
    }
    
    public boolean init() {
        return true;
    }
}
