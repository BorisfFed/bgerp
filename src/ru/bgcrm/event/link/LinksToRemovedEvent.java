package ru.bgcrm.event.link;

import ru.bgcrm.model.CommonObjectLink;
import ru.bgcrm.struts.form.DynActionForm;

public class LinksToRemovedEvent extends LinksToRemovingEvent {
    public LinksToRemovedEvent(DynActionForm form, CommonObjectLink link) {
        super(form, link);
    }
}
