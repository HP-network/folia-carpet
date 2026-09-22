package carpet.folia;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/** Attaches the plugin agent from a short-lived JVM when self-attach is disabled. */
public final class FoliaCarpetAttacher
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 3)
        {
            throw new IllegalArgumentException("Usage: FoliaCarpetAttacher <pid> <agent-jar> <plugin-jar>");
        }
        VirtualMachine vm;
        try
        {
            vm = VirtualMachine.attach(args[0]);
        }
        catch (AttachNotSupportedException | java.io.IOException error)
        {
            System.err.println("Unable to attach to target JVM " + args[0] + ": " + error);
            throw error;
        }
        try
        {
            vm.loadAgent(args[1], args[2]);
        }
        finally
        {
            vm.detach();
        }
    }

    private FoliaCarpetAttacher()
    {
    }
}
