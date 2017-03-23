package it.unibo.studio.unigo.utils;

import android.support.design.widget.TextInputLayout;

public class Error
{
    public static enum Type {
        EMAIL_IS_EMPTY,
        PASSWORD_IS_EMPTY,
        PASSWORD_CONFIRM_IS_EMPTY,
        PASSWORD_WEAK,
        INVALID_CREDENTIALS,
        EMAIL_ALREADY_IN_USE,
        EMAIL_INVALID,
        PASSWORD_MISMATCH,
        NAME_IS_EMPTY,
        LASTNAME_IS_EMPTY,
        PHONE_INCORRECT };

    public static void resetError(TextInputLayout layout)
    {
        layout.setErrorEnabled(false);
        layout.setError(null);
        layout.getEditText().setText("");
    }
}
