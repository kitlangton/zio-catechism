module.exports = {
  extends: ['stylelint-config-recommended'],
  rules: {
    'at-rule-no-unknown': [true, {
      ignoreAtRules: [
        'tailwind',
        'apply',
        'variants',
        'responsive',
        'screen'
      ]
    }],
    'no-descending-specificity': null,
    'max-nesting-depth': 10,
  }
}