package it.unibo.studio.unigo.utils;

import android.support.design.widget.TextInputLayout;

public class Error
{
    public static enum Type {
        EMAIL_IS_EMPTY,
        PASSWORD_IS_EMPTY,
        PASSWORD_CONFIRM_IS_EMPTY,
        INVALID_CREDENTIALS,
        EMAIL_ALREADY_IN_USE,
        EMAIL_INVALID,
        PASSWORD_MISMATCH };

    public static void resetError(TextInputLayout layout)
    {
        layout.setErrorEnabled(false);
        layout.setError(null);
        layout.getEditText().setText("");
    }
}
