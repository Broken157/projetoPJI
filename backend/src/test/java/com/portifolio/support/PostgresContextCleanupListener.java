package com.portifolio.support;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Fecha o pool Spring antes de o Testcontainers encerrar o PostgreSQL da classe. */
public class PostgresContextCleanupListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        // afterTestClass usa a ordem inversa: executar depois dos outros listeners Spring.
        return 0;
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        if (AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), Testcontainers.class)
                && testContext.hasApplicationContext()) {
            testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
        }
    }
}
