import "module" "resources" {
  source = "../../modules/resources.sentinel"
}

mock "tfplan/v2" {
  module {
    source = "../mock-tfplan-pass.sentinel"
  }
}

test {
  rules = {
    main = true
  }
}
