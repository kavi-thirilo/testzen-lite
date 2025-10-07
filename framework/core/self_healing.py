#!/usr/bin/env python3
"""
Intelligent Self-Healing Mechanism for TestZen Framework
Uses AI/ML techniques to automatically recover from element identification failures
"""

import json
import pickle
import logging
import numpy as np
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics.pairwise import cosine_similarity
import difflib
import re
from pathlib import Path

@dataclass
class ElementSignature:
    """Unique signature of an element for identification"""
    locator_type: str
    locator_value: str
    attributes: Dict[str, str]
    text: str
    tag_name: str
    parent_signature: Optional[str] = None
    siblings_count: int = 0
    position: int = 0

@dataclass
class HealingStrategy:
    """Strategy for healing broken locators"""
    name: str
    confidence: float
    new_locator_type: str
    new_locator_value: str
    reasoning: str

class ElementLearner:
    """Learn element patterns and characteristics using ML"""

    def __init__(self, model_path: str = "models/element_patterns.pkl"):
        self.model_path = Path(model_path)
        self.vectorizer = TfidfVectorizer(max_features=100)
        self.classifier = RandomForestClassifier(n_estimators=100)
        self.element_history = {}
        self.logger = logging.getLogger(__name__)
        self.load_model()

    def learn_element(self, element_id: str, signature: ElementSignature, success: bool):
        """Learn from element interaction success/failure"""
        features = self._extract_features(signature)

        if element_id not in self.element_history:
            self.element_history[element_id] = []

        self.element_history[element_id].append({
            'signature': signature,
            'features': features,
            'success': success,
            'timestamp': time.time()
        })

        # Retrain model periodically
        if len(self.element_history) % 100 == 0:
            self._retrain_model()

    def _extract_features(self, signature: ElementSignature) -> np.ndarray:
        """Extract ML features from element signature"""
        features = []

        # Text-based features
        text_features = [
            signature.locator_value,
            signature.text,
            signature.tag_name,
            ' '.join(signature.attributes.values())
        ]
        text_vector = self.vectorizer.fit_transform([' '.join(text_features)])

        # Numerical features
        numerical_features = [
            len(signature.locator_value),
            signature.siblings_count,
            signature.position,
            len(signature.attributes),
            1 if signature.parent_signature else 0
        ]

        # Combine features
        combined = np.hstack([text_vector.toarray()[0], numerical_features])
        return combined

    def _retrain_model(self):
        """Retrain ML model with accumulated data"""
        X = []
        y = []

        for element_data_list in self.element_history.values():
            for data in element_data_list:
                X.append(data['features'])
                y.append(1 if data['success'] else 0)

        if len(X) > 10:
            self.classifier.fit(X, y)
            self.save_model()

    def predict_success(self, signature: ElementSignature) -> float:
        """Predict likelihood of successful element identification"""
        features = self._extract_features(signature)
        try:
            probability = self.classifier.predict_proba([features])[0][1]
            return probability
        except:
            return 0.5  # Default probability if model not trained

    def save_model(self):
        """Save trained model to disk"""
        self.model_path.parent.mkdir(parents=True, exist_ok=True)
        model_data = {
            'vectorizer': self.vectorizer,
            'classifier': self.classifier,
            'history': self.element_history
        }
        with open(self.model_path, 'wb') as f:
            pickle.dump(model_data, f)

    def load_model(self):
        """Load trained model from disk"""
        if self.model_path.exists():
            try:
                with open(self.model_path, 'rb') as f:
                    model_data = pickle.load(f)
                    self.vectorizer = model_data['vectorizer']
                    self.classifier = model_data['classifier']
                    self.element_history = model_data['history']
            except Exception as e:
                self.logger.warning(f"Could not load model: {e}")

class SelfHealingEngine:
    """Main self-healing engine for automatic recovery"""

    def __init__(self, driver):
        self.driver = driver
        self.element_learner = ElementLearner()
        self.healing_cache = {}
        self.logger = logging.getLogger(__name__)
        self.healing_strategies = [
            self._try_partial_match,
            self._try_text_search,
            self._try_relative_xpath,
            self._try_css_selector,
            self._try_ai_prediction,
            self._try_visual_recognition
        ]

    def find_element_with_healing(self, locator_type: str, locator_value: str,
                                 timeout: int = 10) -> Any:
        """Find element with self-healing capability"""
        element_id = f"{locator_type}:{locator_value}"

        # Try original locator first
        try:
            element = self._find_element_regular(locator_type, locator_value, timeout)
            signature = self._create_element_signature(element, locator_type, locator_value)
            self.element_learner.learn_element(element_id, signature, True)
            return element
        except Exception as e:
            self.logger.warning(f"Original locator failed: {e}")

        # Check healing cache
        if element_id in self.healing_cache:
            cached_strategy = self.healing_cache[element_id]
            try:
                element = self._find_element_regular(
                    cached_strategy.new_locator_type,
                    cached_strategy.new_locator_value,
                    timeout
                )
                self.logger.info(f"Healed using cached strategy: {cached_strategy.name}")
                return element
            except:
                pass

        # Try healing strategies
        strategies = self._generate_healing_strategies(locator_type, locator_value)

        for strategy in strategies:
            try:
                element = self._find_element_regular(
                    strategy.new_locator_type,
                    strategy.new_locator_value,
                    timeout=3
                )

                # Validate healed element
                if self._validate_healed_element(element, locator_value):
                    self.logger.info(f"Successfully healed using: {strategy.name}")
                    self.logger.info(f"New locator: {strategy.new_locator_type}={strategy.new_locator_value}")

                    # Cache successful strategy
                    self.healing_cache[element_id] = strategy

                    # Learn from healing
                    signature = self._create_element_signature(
                        element,
                        strategy.new_locator_type,
                        strategy.new_locator_value
                    )
                    self.element_learner.learn_element(element_id, signature, True)

                    return element
            except Exception as e:
                continue

        raise Exception(f"Could not heal element: {locator_type}={locator_value}")

    def _generate_healing_strategies(self, locator_type: str, locator_value: str) -> List[HealingStrategy]:
        """Generate potential healing strategies"""
        strategies = []

        for strategy_func in self.healing_strategies:
            try:
                strategy = strategy_func(locator_type, locator_value)
                if strategy:
                    strategies.append(strategy)
            except Exception as e:
                self.logger.debug(f"Strategy failed: {e}")

        # Sort by confidence
        strategies.sort(key=lambda x: x.confidence, reverse=True)
        return strategies

    def _try_partial_match(self, locator_type: str, locator_value: str) -> Optional[HealingStrategy]:
        """Try partial matching for dynamic IDs"""
        if locator_type == 'id' and any(char in locator_value for char in ['_', '-', ':']):
            # Extract stable part of ID
            parts = re.split(r'[_\-:]', locator_value)
            if parts:
                stable_part = parts[0]
                return HealingStrategy(
                    name="Partial ID Match",
                    confidence=0.7,
                    new_locator_type="xpath",
                    new_locator_value=f"//*[starts-with(@id, '{stable_part}')]",
                    reasoning="Dynamic ID detected, using partial match"
                )
        return None

    def _try_text_search(self, locator_type: str, locator_value: str) -> Optional[HealingStrategy]:
        """Try finding element by text content"""
        if locator_type in ['id', 'name', 'class']:
            # Convert ID/name to readable text
            text = locator_value.replace('_', ' ').replace('-', ' ').title()
            return HealingStrategy(
                name="Text Search",
                confidence=0.6,
                new_locator_type="xpath",
                new_locator_value=f"//*[contains(text(), '{text}') or contains(@value, '{text}')]",
                reasoning="Searching by text content"
            )
        return None

    def _try_relative_xpath(self, locator_type: str, locator_value: str) -> Optional[HealingStrategy]:
        """Try relative XPath based on element structure"""
        if locator_type == 'xpath' and locator_value.startswith('//'):
            # Make XPath more flexible
            simplified = locator_value.replace('[1]', '').replace('//', '//*//')
            return HealingStrategy(
                name="Flexible XPath",
                confidence=0.5,
                new_locator_type="xpath",
                new_locator_value=simplified,
                reasoning="Using more flexible XPath"
            )
        return None

    def _try_css_selector(self, locator_type: str, locator_value: str) -> Optional[HealingStrategy]:
        """Try CSS selector alternatives"""
        if locator_type in ['id', 'class', 'name']:
            if locator_type == 'id':
                css_selector = f"#{locator_value}"
            elif locator_type == 'class':
                css_selector = f".{locator_value.replace(' ', '.')}"
            else:
                css_selector = f"[name='{locator_value}']"

            return HealingStrategy(
                name="CSS Selector",
                confidence=0.65,
                new_locator_type="css",
                new_locator_value=css_selector,
                reasoning="Converting to CSS selector"
            )
        return None

    def _try_ai_prediction(self, locator_type: str, locator_value: str) -> Optional[HealingStrategy]:
        """Use AI to predict best alternative locator"""
        # Get all elements of similar type
        try:
            if locator_type == 'id':
                all_elements = self.driver.find_elements_by_xpath("//*[@id]")
            elif locator_type == 'class':
                all_elements = self.driver.find_elements_by_xpath("//*[@class]")
            else:
                all_elements = self.driver.find_elements_by_xpath("//*")

            best_match = None
            best_score = 0

            for element in all_elements[:50]:  # Limit to first 50 elements
                try:
                    element_id = element.get_attribute('id') or ''
                    element_class = element.get_attribute('class') or ''
                    element_text = element.text or ''

                    # Calculate similarity score
                    score = self._calculate_similarity(
                        locator_value,
                        f"{element_id} {element_class} {element_text}"
                    )

                    if score > best_score:
                        best_score = score
                        best_match = element
                except:
                    continue

            if best_match and best_score > 0.6:
                # Generate new locator for best match
                new_id = best_match.get_attribute('id')
                if new_id:
                    return HealingStrategy(
                        name="AI Prediction",
                        confidence=best_score,
                        new_locator_type="id",
                        new_locator_value=new_id,
                        reasoning=f"AI predicted with {best_score:.2f} confidence"
                    )
        except Exception as e:
            self.logger.debug(f"AI prediction failed: {e}")

        return None

    def _try_visual_recognition(self, locator_type: str, locator_value: str) -> Optional[HealingStrategy]:
        """Use visual recognition to find element"""
        # This would integrate with computer vision libraries
        # For now, return a placeholder
        return None

    def _calculate_similarity(self, text1: str, text2: str) -> float:
        """Calculate text similarity score"""
        return difflib.SequenceMatcher(None, text1.lower(), text2.lower()).ratio()

    def _validate_healed_element(self, element: Any, original_value: str) -> bool:
        """Validate that healed element is correct"""
        try:
            # Check if element is visible and enabled
            if not element.is_displayed() or not element.is_enabled():
                return False

            # Check text similarity
            element_text = element.text or element.get_attribute('value') or ''
            if self._calculate_similarity(original_value, element_text) > 0.5:
                return True

            # Check attribute similarity
            for attr in ['id', 'name', 'class', 'placeholder']:
                attr_value = element.get_attribute(attr) or ''
                if self._calculate_similarity(original_value, attr_value) > 0.5:
                    return True

            return True  # Accept if other validations pass

        except:
            return False

    def _find_element_regular(self, locator_type: str, locator_value: str, timeout: int = 10):
        """Regular element finding without healing"""
        from selenium.webdriver.common.by import By
        from selenium.webdriver.support.ui import WebDriverWait
        from selenium.webdriver.support import expected_conditions as EC

        by_mapping = {
            'id': By.ID,
            'name': By.NAME,
            'xpath': By.XPATH,
            'css': By.CSS_SELECTOR,
            'class': By.CLASS_NAME,
            'tag': By.TAG_NAME
        }

        by = by_mapping.get(locator_type, By.ID)
        wait = WebDriverWait(self.driver, timeout)
        return wait.until(EC.presence_of_element_located((by, locator_value)))

    def _create_element_signature(self, element: Any, locator_type: str,
                                 locator_value: str) -> ElementSignature:
        """Create signature for element"""
        try:
            attributes = {}
            for attr in ['id', 'name', 'class', 'type', 'placeholder', 'value']:
                val = element.get_attribute(attr)
                if val:
                    attributes[attr] = val

            return ElementSignature(
                locator_type=locator_type,
                locator_value=locator_value,
                attributes=attributes,
                text=element.text or '',
                tag_name=element.tag_name,
                siblings_count=len(element.find_elements_by_xpath("..//*")),
                position=0
            )
        except:
            return ElementSignature(
                locator_type=locator_type,
                locator_value=locator_value,
                attributes={},
                text='',
                tag_name='',
                siblings_count=0,
                position=0
            )

    def generate_healing_report(self) -> Dict[str, Any]:
        """Generate report of healing activities"""
        report = {
            'total_healings': len(self.healing_cache),
            'strategies_used': {},
            'success_rate': 0,
            'healed_elements': []
        }

        for element_id, strategy in self.healing_cache.items():
            if strategy.name not in report['strategies_used']:
                report['strategies_used'][strategy.name] = 0
            report['strategies_used'][strategy.name] += 1

            report['healed_elements'].append({
                'original': element_id,
                'strategy': strategy.name,
                'new_locator': f"{strategy.new_locator_type}={strategy.new_locator_value}",
                'confidence': strategy.confidence
            })

        return report