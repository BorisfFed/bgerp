package ru.bgcrm.event.customer;

import ru.bgcrm.event.UserEvent;
import ru.bgcrm.struts.form.DynActionForm;

public class CustomerChangedEvent extends UserEvent {
    private int customerId;

    public CustomerChangedEvent(DynActionForm form, int customerId) {
        super(form);
        this.customerId = customerId;
    }

    public int getCustomerId() {
        return customerId;
    }
}
