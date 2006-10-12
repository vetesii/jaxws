package fromjava.stateful.bank;

import com.sun.xml.ws.developer.ServerFeatures;
import com.sun.xml.ws.developer.StatefulWebServiceManager;
import com.sun.xml.ws.developer.StatefulWebServiceManager.Callback;

import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.Feature;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.AddressingFeature;

/**
 * @author Kohsuke Kawaguchi
 */
@WebService
@BindingType(features={@Feature(ServerFeatures.STATEFUL),@Feature(AddressingFeature.ID)})
public class Account {
    private final int id;
    private int amount;

    public Account(int id) {
        this.id = id;
    }

    public void deposit(int delta) {
        amount += delta;
    }

    public int getBalance() {
        return amount;
    }

    public int getId() {
        return id;
    }

    public void destroy() {
        manager.unexport(this);
    }

    /*package*/ static StatefulWebServiceManager<Account> manager;

    // to be injected by JAX-WS RI
    public static void setStatefulManager(StatefulWebServiceManager<Account> m) {
        manager = m;
        manager.setTimeout(1000,new Callback<Account>() {
            public void onTimeout(Account o, StatefulWebServiceManager manager) {
                if(o.id<10000)
                    manager.touch(o);
                else
                    manager.unexport(o);
            }
        });
        manager.setFallbackInstance(new Account(-1) {
            public int getBalance() {
                throw new WebServiceException("no such account");
            }
        });
    }
}
