import copy
import importlib.util
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('validator',ROOT/'tools/validate_feature_matrix.py')
v=importlib.util.module_from_spec(spec); spec.loader.exec_module(v)
class FeatureInventoryTest(unittest.TestCase):
    def setUp(self): self.data=json.loads((ROOT/'feature_matrix.json').read_text())
    def test_valid_inventory(self): self.assertEqual([],v.validate(self.data))
    def test_missing_id_rejected(self): self.data['features'].pop(); self.assertTrue(v.validate(self.data))
    def test_duplicate_id_rejected(self): self.data['features'][1]['id']='001'; self.assertTrue(v.validate(self.data))
    def test_invented_status_rejected(self): self.data['features'][0]['implementation_status']='COMPLETE'; self.assertTrue(v.validate(self.data))
    def test_verified_without_evidence_rejected(self): self.data['features'][0]['implementation_status']='DONE_VERIFIED'; self.assertTrue(v.validate(self.data))
    def test_bad_totals_rejected(self): self.data['counts']['PARTIAL']=900; self.assertTrue(v.validate(self.data))
    def test_missing_field_rejected(self): del self.data['features'][0]['limitations']; self.assertTrue(v.validate(self.data))
    def test_bad_source_path_rejected(self): self.data['features'][0]['source_file']=['does-not-exist.kt']; self.assertTrue(v.validate(self.data))
if __name__=='__main__': unittest.main()
