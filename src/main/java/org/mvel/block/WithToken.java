package org.mvel.block;

import org.mvel.CompileException;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;
import static org.mvel.MVEL.compileExpression;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.balancedCapture;
import static org.mvel.util.ParseTools.subset;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christopher Brock
 */
public class WithToken extends BlockToken {

    private String name;
    private ParmValuePair[] withExpressions;

    public WithToken(char[] expr, char[] block, int fields) {
        super(expr, fields, block);
        name = new String(expr).trim();

        System.out.println("<<" + new String(block) + ">>");

        compileWithExpressions();
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        VariableResolver resolver = factory.getVariableResolver(name);
        if (resolver == null) {
            throw new CompileException("no such variable for with block: " + name);
        }

        Object ctxObject = resolver.getValue();

        for (ParmValuePair pvp : withExpressions) {
            MVEL.setProperty(ctxObject, pvp.getParameter(), pvp.getStatement().getValue(ctx, thisValue, factory));
        }

        return Void.class;
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }

    private void compileWithExpressions() {
        List<ParmValuePair> parms = new ArrayList<ParmValuePair>();

        int start = 0;
        String parm = "";

        for (int i = 0; i < block.length; i++) {
            switch (block[i]) {
                case'{':
                case'[':
                case'(':
                    if ((i = balancedCapture(block, i, block[i])) == -1) {
                        throw new CompileException("unbalanced braces", block, i);
                    }
                    break;

                case'=':
                    parm = new String(block, start, i - start).trim();
                    start = ++i;
                    break;

                case',':
                    if (parm == null) {
                        throw new CompileException("invalid syntax, expected =", block, i);
                    }

                    parms.add(new ParmValuePair(
                            parm,
                            (ExecutableStatement) compileExpression(subset(block, start, i - start)))
                    );

                    parm = null;
                    start = ++i;

                    break;
            }
        }

        if (parm != null && start != block.length) {
            parms.add(new ParmValuePair(
                    parm,
                    (ExecutableStatement) compileExpression(subset(block, start, block.length - start)))
            );
        }

        parms.toArray(withExpressions = new ParmValuePair[parms.size()]);
    }

    public static final class ParmValuePair {
        private String parameter;
        private ExecutableStatement statement;

        public ParmValuePair() {
        }

        public ParmValuePair(String parameter, ExecutableStatement statement) {
            System.out.println("adding parm [" + parameter + "]");
            this.parameter = parameter;
            this.statement = statement;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public ExecutableStatement getStatement() {
            return statement;
        }

        public void setStatement(ExecutableStatement statement) {
            this.statement = statement;
        }
    }

}
