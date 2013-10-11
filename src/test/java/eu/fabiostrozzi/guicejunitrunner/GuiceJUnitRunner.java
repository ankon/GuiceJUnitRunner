// GuiceJunitRunner.java, created by Fabio Strozzi on Mar 27, 2011
package eu.fabiostrozzi.guicejunitrunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.internal.runners.statements.Fail;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * A JUnit class runner for Guice based applications.
 * 
 * A test class can control the injection in multiple ways:
 * <ol>
 * <li>implement {@link Module}</li>
 * <li>use the {@link GuiceModules} annotation on the class</li>
 * <li>use the {@link GuiceModules} annotation on a single method</li>
 * </ol>
 * 
 * @author Fabio Strozzi
 */
public class GuiceJUnitRunner extends BlockJUnit4ClassRunner {
    private Injector injector;

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface GuiceModules {
        Class<?>[] value();
    }
    
    @Override
    public Object createTest() throws Exception {
        Object obj = super.createTest();
        
        Injector thisInjector = injector;
        if (obj instanceof Module) {
            thisInjector = thisInjector.createChildInjector((Module) obj);
        }
        thisInjector.injectMembers(obj);
        
        return obj;
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        // If the specific method has a module annotation, pull that in as well.
        GuiceModules annotation = method.getAnnotation(GuiceModules.class);
        if (annotation != null) {
            try {
                Module[] modules = createModules(annotation.value());
                injector.createChildInjector(modules).injectMembers(test);
            } catch (Throwable e) {
                return new Fail(e);
            }
        }

        return super.methodInvoker(method, test);
    }

    /**
     * Instances a new JUnit runner.
     * 
     * @param klass
     *            The test class
     * @throws InitializationError
     */
    public GuiceJUnitRunner(Class<?> klass) throws InitializationError {
        super(klass);
        Class<?>[] classes = getModulesFor(klass);
        
        injector = Guice.createInjector(createModules(classes));
    }

    /**
     * @param classes
     * @return
     * @throws InitializationError
     */
    private Module[] createModules(Class<?>[] classes) throws InitializationError {
        Module[] modules = new Module[classes.length];
        for (int i = 0; i < classes.length; i++) {
            try {
                modules[i] = (Module) (classes[i]).newInstance();
            } catch (InstantiationException e) {
                throw new InitializationError(e);
            } catch (IllegalAccessException e) {
                throw new InitializationError(e);
            }
        }

        return modules;
    }
    
    /**
     * Gets the Guice modules for the given test class.
     * 
     * @param klass
     *            The test class
     * @return The array of Guice {@link Module} modules used to initialize the
     *         injector for the given test.
     */
    private Class<?>[] getModulesFor(Class<?> klass) {
        // The annotation might be missing, that's fine. It saves having to think about whether to use
        // the @RunWith or not. 
        GuiceModules annotation = klass.getAnnotation(GuiceModules.class);
        if (annotation != null) {
            return annotation.value(); 
        }

        return new Class<?>[0];
    }

}
