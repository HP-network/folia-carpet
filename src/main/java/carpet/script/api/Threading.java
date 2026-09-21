package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;
import carpet.folia.FoliaRuntime;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletionException;

public class Threading
{
    public static void apply(Expression expression)
    {

        expression.addContextFunction("task_join", 1, (c, t, lv) -> {
            if (((CarpetContext) c).server().isSameThread())
            {
                throw new InternalExpressionException("'task_join' cannot be called from main thread to avoid deadlocks");
            }
            Value v = lv.get(0);
            if (!(v instanceof final ThreadValue tv))
            {
                throw new InternalExpressionException("'task_join' could only be used with a task value");
            }
            return tv.join();
        });

        expression.addLazyFunctionWithDelegation("task_dock", 1, false, true, (c, t, expr, tok, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            MinecraftServer server = cc.server();
            if (server.isSameThread() || FoliaRuntime.plugin() != null)
            {
                return (ct, tt) -> lv.get(0).evalValue(ct, tt);
            }
            Value[] result = new Value[]{Value.NULL};
            RuntimeException[] internal = new RuntimeException[]{null};
            try
            {
                result[0] = lv.get(0).evalValue(c, t);
            }
            catch (ExpressionException exc)
            {
                internal[0] = exc;
            }
            catch (InternalExpressionException exc)
            {
                internal[0] = new ExpressionException(c, expr, tok, exc.getMessage(), exc.stack);
            }
            catch (ArithmeticException exc)
            {
                internal[0] = new ExpressionException(c, expr, tok, "Your math is wrong, " + exc.getMessage());
            }
            if (internal[0] != null)
            {
                throw internal[0];
            }
            Value ret = result[0];
            return (ct, tt) -> ret;

        });
    }
}
