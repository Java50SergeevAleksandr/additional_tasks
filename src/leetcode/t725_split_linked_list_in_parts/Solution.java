package leetcode.t725_split_linked_list_in_parts;

public class Solution {
	public class ListNode {
		int val;
		ListNode next;

		ListNode() {
		}

		ListNode(int val) {
			this.val = val;
		}

		ListNode(int val, ListNode next) {
			this.val = val;
			this.next = next;
		}
	}

	public ListNode[] splitListToParts(ListNode head, int k) {
		ListNode[] output = new ListNode[k];

		if (head == null) {
			return output;
		}

		int[] sizes = findSize(head, k);
		fillLists(head, output, sizes);

		return output;
	}

	private int[] findSize(ListNode head, int k) {
		int baseListSize = 0;
		ListNode currentNode = head;
		int[] partListSizes = new int[k];

		while (currentNode != null) {
			baseListSize++;
			currentNode = currentNode.next;
		}

		int partSize = 0;

		for (int i = k; i > 0; i--) {
			baseListSize -= partSize;
			partSize = baseListSize / i;
			int remainder = baseListSize % i;

			if (remainder != 0) {
				partSize++;
			}

			partListSizes[k - i] = partSize;
		}

		return partListSizes;
	}

	private void fillLists(ListNode head, ListNode[] output, int[] sizes) {
		ListNode currentNode = head;

		for (int i = 0; i < sizes.length; i++) {
			if (sizes[i] != 0) {
				output[i] = new ListNode(currentNode.val);
				ListNode subNodeCurrent = output[i];
				for (int j = 0; j < sizes[i] - 1; j++) {
					if (currentNode.next != null) {
						currentNode = currentNode.next;
						subNodeCurrent.next = new ListNode(currentNode.val);
						subNodeCurrent = subNodeCurrent.next;
					}
				}
				if (currentNode.next != null) {
					currentNode = currentNode.next;
				}
			}
		}
	}
}
