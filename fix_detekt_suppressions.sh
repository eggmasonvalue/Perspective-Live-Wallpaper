#!/bin/bash
# Re-enable the rules we temporarily disabled
sed -i 's/CyclomaticComplexMethod:\n    active: false/CyclomaticComplexMethod:\n    active: true/' config/detekt/detekt.yml
sed -i 's/TooManyFunctions:\n    active: false/TooManyFunctions:\n    active: true/' config/detekt/detekt.yml
sed -i 's/SwallowedException:\n    active: false/SwallowedException:\n    active: true/' config/detekt/detekt.yml
sed -i 's/TooGenericExceptionCaught:\n    active: false/TooGenericExceptionCaught:\n    active: true/' config/detekt/detekt.yml
