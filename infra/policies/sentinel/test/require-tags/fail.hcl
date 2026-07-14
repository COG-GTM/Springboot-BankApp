import "module" "resources" {
  source = "../../modules/resources.sentinel"
}

mock "tfplan/v2" {
  module {
    source = "../mock-tfplan-fail.sentinel"
  }
}

test {
  rules = {
    main = false
  }
}
