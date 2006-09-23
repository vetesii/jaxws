/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
package fromwsdl_wsaddressing.client;

public class AddNumbersClient {


    int number1 = 10;
    int number2 = 10;
    int negativeNumber1 = -10;

    public static void main(String[] args) {
        System.out.printf("From WSDL Sample application\n");
        AddNumbersClient client = new AddNumbersClient();
        client.addNumbers();
        client.addNumbersFault();
        client.addNumbers2();
        client.addNumbers3();
        client.addNumbers3Fault();
    }


    public void addNumbers() {
        System.out.printf("addNumbers: default input and output names\n");
        try {
            AddNumbersPortType stub = createStub();
            int result = stub.addNumbers(number1, number2);
            assert result == 20;
        } catch (Exception ex) {
            ex.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbersFault() {
        System.out.printf("addNumbers: default fault name, invalid operation input value\n");
        AddNumbersPortType stub = null;
        try {
            stub = createStub();
            int result = stub.addNumbers(-10, 10);
            assert false;
        } catch (AddNumbersFault_Exception ex) {
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbers2() {
        System.out.printf("addNumbers2: custom input and output names\n");
        try {
            AddNumbersPortType stub = createStub();
            int result = stub.addNumbers2(10, 10);
            assert result == 20;
        } catch (Exception ex) {
            ex.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbers3() {
        System.out.printf("addNumbers3: custom input and output names\n");
        try {
            AddNumbersPortType stub = createStub();
            int result = stub.addNumbers3(10, 10);
            assert result == 20;
        } catch (Exception ex) {
            ex.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbers3Fault() {
        System.out.printf("addNumbers3: custom fault and invalid input value\n");
        AddNumbersPortType stub = null;

        try {
            stub = createStub();
            int result = stub.addNumbers3(-10, 10);
            assert false;
        } catch (AddNumbersFault_Exception ex) {
            System.out.printf("Exception value is \"%s\"\n", ex.toString());
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }


    private AddNumbersPortType createStub() throws Exception {
        AddNumbersService service = new AddNumbersService();
        AddNumbersPortType stub = service.getAddNumbersPort();

        return stub;
    }
}
