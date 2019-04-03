package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class TypeResolverTest {

    private static final byte[] FOO = new byte[]{1, 2, 3};

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer typeInitializer, otherTypeInitializer;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ClassLoadingStrategy classLoadingStrategy;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Before
    public void setUp() throws Exception {
        when(dynamicType.getTypeDescription()).thenReturn(typeDescription);
        when(dynamicType.getAllTypes()).thenReturn(Collections.singletonMap(typeDescription, FOO));
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(typeDescription, loadedTypeInitializer));
        when(classLoadingStrategy.load(classLoader, Collections.singletonMap(typeDescription, FOO)))
                .thenReturn(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class));
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        when(typeDescription.getName()).thenReturn(Foo.class.getName());
    }

    @Test
    public void testPassive() throws Exception {
        TypeResolver.Resolved resolved = TypeResolver.Passive.INSTANCE.resolve();
        assertThat(resolved.injectedInto(typeInitializer), is(typeInitializer));
        assertThat(resolved.initialize(dynamicType, classLoader, classLoadingStrategy),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class)));
        verify(classLoadingStrategy).load(classLoader, Collections.singletonMap(typeDescription, FOO));
        verifyNoMoreInteractions(classLoadingStrategy);
        verify(loadedTypeInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(loadedTypeInitializer);
    }

    @Test
    public void testActive() throws Exception {
        TypeResolver.Resolved resolved = TypeResolver.Active.INSTANCE.resolve();
        Field field = TypeResolver.Active.Resolved.class.getDeclaredField("identification");
        field.setAccessible(true);
        int identification = (Integer) field.get(resolved);
        when(typeInitializer.expandWith(new TypeResolver.Active.InitializationAppender(identification))).thenReturn(otherTypeInitializer);
        assertThat(resolved.injectedInto(typeInitializer), is(otherTypeInitializer));
        assertThat(resolved.initialize(dynamicType, classLoader, classLoadingStrategy),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class)));
        try {
            verify(classLoadingStrategy).load(classLoader, Collections.singletonMap(typeDescription, FOO));
            verifyNoMoreInteractions(classLoadingStrategy);
            verify(loadedTypeInitializer).isAlive();
            verifyNoMoreInteractions(loadedTypeInitializer);
        } finally {
            Field initializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
            initializers.setAccessible(true);
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) initializers.get(null)).remove(constructor.newInstance(Foo.class.getName(), Foo.class.getClassLoader(), identification));
            assertThat(value, CoreMatchers.is((Object) loadedTypeInitializer));
        }
    }

    @Test
    public void testLazy() throws Exception {
        TypeResolver.Resolved resolved = TypeResolver.Lazy.INSTANCE.resolve();
        assertThat(resolved.injectedInto(typeInitializer), is(typeInitializer));
        assertThat(resolved.initialize(dynamicType, classLoader, classLoadingStrategy),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class)));
        verify(classLoadingStrategy).load(classLoader, Collections.singletonMap(typeDescription, FOO));
        verifyNoMoreInteractions(classLoadingStrategy);
        verifyNoMoreInteractions(loadedTypeInitializer);
    }

    @Test
    public void testDisabled() throws Exception {
        TypeResolver.Resolved resolved = TypeResolver.Disabled.INSTANCE.resolve();
        assertThat(resolved.injectedInto(typeInitializer), is(typeInitializer));
    }

    @Test(expected = IllegalStateException.class)
    public void testDisabledCannotBeApplied() throws Exception {
        TypeResolver.Disabled.INSTANCE.resolve().initialize(dynamicType, classLoader, classLoadingStrategy);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeResolver.Active.class).apply();
        ObjectPropertyAssertion.of(TypeResolver.Active.Resolved.class).apply();
        ObjectPropertyAssertion.of(TypeResolver.Passive.class).apply();
        ObjectPropertyAssertion.of(TypeResolver.Lazy.class).apply();
        ObjectPropertyAssertion.of(TypeResolver.Disabled.class).apply();
    }

    private static class Foo {
        /* empty */
    }
}
