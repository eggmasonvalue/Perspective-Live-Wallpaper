#!/bin/bash
sed -i 's/ImplicitDefaultLocale:\n    active: false/ImplicitDefaultLocale:\n    active: true/' config/detekt/detekt.yml
sed -i 's/MagicNumber:\n    active: false/MagicNumber:\n    active: true/' config/detekt/detekt.yml
sed -i 's/MaxLineLength:\n    active: false/MaxLineLength:\n    active: true/' config/detekt/detekt.yml
