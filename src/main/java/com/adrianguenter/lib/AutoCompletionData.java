package com.adrianguenter.lib;

import javax.swing.*;

public record AutoCompletionData(
        FqnType type,
        Icon icon,
        String tailText,
        String typeText,
        String lookupString
) {
}

