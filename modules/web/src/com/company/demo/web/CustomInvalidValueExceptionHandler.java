package com.company.demo.web;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.exception.AbstractExceptionHandler;
import com.haulmont.cuba.web.toolkit.ui.CubaTextField;
import com.haulmont.cuba.web.toolkit.ui.converters.StringToDatatypeConverter;
import com.vaadin.data.Validator;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class CustomInvalidValueExceptionHandler extends AbstractExceptionHandler {

    public CustomInvalidValueExceptionHandler() {
        super(Validator.InvalidValueException.class.getName());
    }

    @Override
    protected void doHandle(App app, String className, String message, @Nullable Throwable throwable) {
    }

    @Override
    public boolean handle(ErrorEvent event, App app) {
        boolean handled = super.handle(event, app);

        //noinspection ThrowableResultOfMethodCallIgnored
        if (handled && event.getThrowable() != null) {
            boolean customErrorMessage = false;

            // Finds the original source of the error/exception
            AbstractComponent component = DefaultErrorHandler.findAbstractComponent(event);
            if (component != null) {
                component.markAsDirty();

                if (component instanceof CubaTextField) {
                    CubaTextField textField = (CubaTextField) component;

                    if (textField.getConverter() instanceof StringToDatatypeConverter) {
                        Datatype datatype = getDatatype(textField);

                        if (Integer.class.equals(datatype.getJavaClass())) {
                            app.getWindowManager().showNotification(
                                    "Input error",
                                    "Must be integer",
                                    Frame.NotificationType.TRAY
                            );

                            customErrorMessage = true;
                        }
                    }
                }
            }

            if (component instanceof Component.Focusable) {
                ((Component.Focusable) component).focus();
            }

            //noinspection ThrowableResultOfMethodCallIgnored
            if (event.getThrowable() instanceof Validator.InvalidValueException) {
                app.getAppUI().discardAccumulatedEvents();
            }

            if (!customErrorMessage) {
                Messages messages = AppBeans.get(Messages.NAME);
                app.getWindowManager().showNotification(
                        messages.getMainMessage("validationFail.caption"),
                        messages.getMainMessage("validationFail"),
                        Frame.NotificationType.TRAY
                );
            }
        }
        return handled;
    }

    protected Datatype getDatatype(CubaTextField textField) {
        // CAUTION! hack with reflection
        Datatype datatype;
        try {
            Field field = FieldUtils.getDeclaredField(StringToDatatypeConverter.class, "datatype", true);

            datatype = (Datatype) field.get(textField.getConverter());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to read datatype of converter", e);
        }
        return datatype;
    }
}