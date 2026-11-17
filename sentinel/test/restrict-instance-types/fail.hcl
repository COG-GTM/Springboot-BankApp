# Test: Instances use unapproved types — should FAIL

mock "tfplan/v2" {
  module {
    source = "mock-tfplan-fail.sentinel"
  }
}

test {
  rules = {
    main = false
  }
}
